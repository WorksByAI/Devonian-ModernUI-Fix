from pathlib import Path
import hashlib
import json
import tempfile
import unittest
import zipfile
from prepare_fixtures import prepare


class FixtureTests(unittest.TestCase):
    def fixture(self, root, mod_id, version):
        path = root / (mod_id + '.jar')
        package, name = ('icyllis/modernui/mc/text/mixin', 'MixinGuiTextRenderState') if mod_id == 'modernui' else ('com/github/synnerz/devonian/mixin', 'GuiTextRenderStateMixin')
        with zipfile.ZipFile(path, 'w') as jar:
            jar.writestr('fabric.mod.json', json.dumps({'id': mod_id, 'version': version, 'entrypoints': {'client': ['never.Run']}}))
            jar.writestr(package + '/' + name + '.class', b'original class bytes')
            jar.writestr('com/github/synnerz/devonian/GuiTextRenderStateAccessor.class', b'accessor')
            jar.writestr('never/Run.class', b'not a test entrypoint')
        return path

    def test_preserves_versions_and_original_bytes_without_mod_initializers(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            modern = self.fixture(root, 'modernui', '3.13.0.5')
            for version in ['1.32.9', '1.33.9', '1.34.9']:
                devonian = self.fixture(root, 'devonian', version)
                before = devonian.read_bytes()
                records = prepare(modern, devonian, root / 'out')
                self.assertEqual(before, devonian.read_bytes())
                self.assertEqual(version, records[1]['version'])
                self.assertEqual(hashlib.sha256(before).hexdigest(), records[1]['sha256'])
                with zipfile.ZipFile(root / 'out/libs/devonian-fixture.jar') as jar:
                    metadata = json.loads(jar.read('fabric.mod.json'))
                    self.assertEqual(version, metadata['version'])
                    self.assertNotIn('entrypoints', metadata)
                    self.assertNotIn('never/Run.class', jar.namelist())
                    self.assertEqual(b'original class bytes', jar.read('com/github/synnerz/devonian/mixin/GuiTextRenderStateMixin.class'))

    def test_rejects_untested_versions_before_writing(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            modern = self.fixture(root, 'modernui', '3.13.0.5')
            devonian = self.fixture(root, 'devonian', '1.35.9')
            with self.assertRaises(ValueError): prepare(modern, devonian, root / 'out')
            self.assertFalse((root / 'out').exists())

    def test_rejects_unknown_modern_ui_versions(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            modern = self.fixture(root, 'modernui', '4.0.0')
            devonian = self.fixture(root, 'devonian', '1.34.9')
            with self.assertRaises(ValueError): prepare(modern, devonian, root / 'out')


if __name__ == '__main__':
    unittest.main()
