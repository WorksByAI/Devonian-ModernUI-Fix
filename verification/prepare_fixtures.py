"""Extract original published class bytes for isolated tests; never edits input jars.

Usage: python prepare_fixtures.py MODERNUI_JAR DEVONIAN_JAR
Fixtures keep only the GUI text mixins active, with no mod initializers.
Generated jars are local research dependencies and must not be redistributed.
"""
from pathlib import Path
import hashlib
import json
import sys
import zipfile

SUPPORTED_DEVONIAN = frozenset({'1.32.9', '1.33.9', '1.34.9'})


def prepare_one(source_path, mod_id, supported, package, class_name, libraries):
    mixin_config = mod_id + '-fixture.mixins.json'
    with zipfile.ZipFile(source_path) as source:
        metadata = json.loads(source.read('fabric.mod.json'))
        if metadata.get('id') != mod_id or metadata.get('version') not in supported:
            raise ValueError('Unsupported ' + mod_id + ' identity/version: ' + str(metadata.get('version')))
        version = metadata['version']
        mixin_bytes = source.read(package.replace('.', '/') + '/' + class_name + '.class')
        with zipfile.ZipFile(libraries / (mod_id + '-fixture.jar'), 'w', zipfile.ZIP_DEFLATED) as dest:
            for entry in source.infolist():
                if entry.filename == 'fabric.mod.json':
                    continue
                if mod_id == 'devonian' and entry.filename not in [
                    package.replace('.', '/') + '/' + class_name + '.class',
                    'com/github/synnerz/devonian/GuiTextRenderStateAccessor.class',
                ]:
                    continue
                dest.writestr(entry, source.read(entry))
            dest.writestr('fabric.mod.json', json.dumps({
                'schemaVersion': 1, 'id': mod_id, 'version': version, 'environment': 'client',
                'mixins': [mixin_config],
            }))
            config = {
                'required': True, 'package': package, 'compatibilityLevel': 'JAVA_25',
                'client': [class_name], 'injectors': {'defaultRequire': 1},
            }
            if mod_id == 'modernui':
                config['plugin'] = 'verification.BoundaryPlugin'
            dest.writestr(mixin_config, json.dumps(config))
    return {'id': mod_id, 'version': version, 'sha256': hashlib.sha256(source_path.read_bytes()).hexdigest(),
            'mixinSha256': hashlib.sha256(mixin_bytes).hexdigest()}


def prepare(modernui, devonian, output=None):
    root = Path(output) if output is not None else Path(__file__).resolve().parent
    inputs = [
        (Path(modernui), 'modernui', {'3.13.0.5'}, 'icyllis.modernui.mc.text.mixin', 'MixinGuiTextRenderState'),
        (Path(devonian), 'devonian', SUPPORTED_DEVONIAN, 'com.github.synnerz.devonian.mixin', 'GuiTextRenderStateMixin'),
    ]
    # Reject unknown metadata or missing mixins before replacing any fixture files.
    for path, mod_id, versions, package, name in inputs:
        with zipfile.ZipFile(path) as jar:
            metadata = json.loads(jar.read('fabric.mod.json'))
            if metadata.get('id') != mod_id or metadata.get('version') not in versions:
                raise ValueError('Unsupported ' + mod_id + ' identity/version: ' + str(metadata.get('version')))
            jar.read(package.replace('.', '/') + '/' + name + '.class')
    libraries = root / 'libs'
    libraries.mkdir(parents=True, exist_ok=True)
    records = [prepare_one(*entry, libraries) for entry in inputs]
    (root / 'fixture-hashes.json').write_text(json.dumps(records, indent=2), encoding='utf-8')
    return records


if __name__ == '__main__':
    prepare(*sys.argv[1:])
    print('Prepared original mixin fixtures without original mod entrypoints.')
