// Karma configuration for the Angular dashboard.
// Based on the Angular CLI template. The CLI's `@angular-devkit/build-angular:karma`
// builder loads this file and injects build-specific files/preprocessors on top,
// but frameworks/plugins must be declared here for the test run to initialize.

const path = require('path');

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma'),
    ],
    client: {
      jasmine: {},
      clearContext: false, // leave Jasmine Spec Runner output visible in browser
    },
    jasmineHtmlReporter: {
      suppressAllSummary: true, // suppress failed, no info messages
      suppressFailedSummary: false,
    },
    coverageReporter: {
      dir: path.join(__dirname, './coverage/frontend-angular'),
      subdir: '.',
      reporters: [{ type: 'html' }, { type: 'text-summary' }],
    },
    reporters: ['progress', 'kjhtml'],
    browsers: ['ChromeHeadless'],
    // CI-friendly launcher: Chrome's sandbox is unavailable on GitHub Actions
    // ubuntu runners. Use via `ng test --browsers=ChromeHeadlessNoSandbox`.
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage'],
      },
    },
    restartOnFileChange: false,
    singleRun: true,
  });
};
