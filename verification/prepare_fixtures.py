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

root = Path(__file__).resolve().parent
libraries = root / 'libs'
libraries.mkdir(exist_ok=True)
records = []
for source_path, mod_id, version, package, class_name in [
    (Path(sys.argv[1]), 'modernui', '3.13.0.5', 'icyllis.modernui.mc.text.mixin', 'MixinGuiTextRenderState'),
    (Path(sys.argv[2]), 'devonian', '1.32.9', 'com.github.synnerz.devonian.mixin', 'GuiTextRenderStateMixin'),
]:
    mixin_config = mod_id + '-fixture.mixins.json'
    with zipfile.ZipFile(source_path) as source:
        metadata = json.loads(source.read('fabric.mod.json'))
        assert metadata['id'] == mod_id and metadata['version'] == version
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
    records.append({'id': mod_id, 'version': version, 'sha256': hashlib.sha256(source_path.read_bytes()).hexdigest()})
(root / 'fixture-hashes.json').write_text(json.dumps(records, indent=2), encoding='utf-8')
print('Prepared original mixin fixtures without original mod entrypoints.')
