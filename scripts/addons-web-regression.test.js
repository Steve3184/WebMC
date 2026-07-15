'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const webDir = path.resolve(__dirname, '..', 'addons', 'web');

function readWebScript(name) {
    return fs.readFileSync(path.join(webDir, name), 'utf8');
}

test('mcperf script parses', () => {
    assert.doesNotThrow(() => {
        new vm.Script(readWebScript('mcperf.js'), { filename: 'addons/web/mcperf.js' });
    });
});

test('VFS notifies ready listeners after initialization fails', () => {
    let readyCalls = 0;

    class FailingRequest {
        open() {}

        send() {
            this.onerror();
        }
    }

    const window = {
        VFS_URL: '/missing.mcvf.gz',
        VFS_ONREADY() {
            readyCalls += 1;
        }
    };

    vm.runInNewContext(
        readWebScript('vfs.js'),
        {
            console: { error() {} },
            window,
            XMLHttpRequest: FailingRequest
        },
        { filename: 'addons/web/vfs.js' }
    );

    assert.equal(readyCalls, 1);
});
