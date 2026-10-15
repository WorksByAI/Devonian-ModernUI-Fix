"""Run the real released mixins against Minecraft, before the graphics boundary.

Usage: python verification/run_matrix.py MODERNUI_JAR DEVONIAN_JAR [DEVONIAN_JAR ...]
Requires Java 25 and a built patch. Input jars are never modified or distributed.
"""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import zipfile
from prepare_fixtures import prepare


def run(modernui, *devonian_jars):
    verification = Path(__file__).resolve().parent
    project = verification.parent
    output = verification / 'build/matrix'
    output.mkdir(parents=True, exist_ok=True)
    records = []
    for filename in devonian_jars:
        path = Path(filename).resolve()
        release = prepare(modernui, path)
        version = release[1]['version']
        modes = [('baseline', ['-Pbaseline']), ('patched', []), ('disabled', ['-PtextEngineDisabled'])]
        if version == '1.34.9':
            modes.append(('loader-0.19.5', ['-PloaderVersion=0.19.5']))
        for mode, arguments in modes:
            label = version + ('-loader-0.19.5' if mode == 'loader-0.19.5' else '')
            folder_mode = 'patched' if mode == 'loader-0.19.5' else mode
            result_file = verification / ('run-' + label + '-' + folder_mode) / 'verification-result.txt'
            if result_file.exists(): result_file.unlink()
            log = output / (version + '-' + mode + '.log')
            command = [str(project/'gradlew.bat'), '-p', str(verification), 'runVerify', '-x', 'downloadAssets',
                       '-PverificationLabel=' + label, *arguments, '--console=plain']
            if os.name == 'nt': command = ['cmd.exe', '/d', '/c', *command]
            with log.open('w', encoding='utf-8') as stream:
                result = subprocess.run(command, cwd=project, stdout=stream, stderr=subprocess.STDOUT, timeout=180)
            if result.returncode:
                raise AssertionError(log.read_text(encoding='utf-8')[-6000:])
            report = result_file.read_text(encoding='utf-8')
            if not report.startswith('PASS: 48 assertions.'): raise AssertionError(report)
            records.append({'devonian':version,'mode':mode,'result':report.strip(),'inputs':release})
            print('PASS Devonian ' + version + ' / ' + mode + ' / 48 assertions', flush=True)
    jar = project / 'build/libs/devonian-modernui-fix-1.0.1+26.1.2.jar'
    summary = {'patchSha256':hashlib.sha256(jar.read_bytes()).hexdigest(), 'runs':records}
    (output/'result.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
    return summary


if __name__ == '__main__':
    run(*sys.argv[1:])
