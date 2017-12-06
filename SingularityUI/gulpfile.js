var gulp = require('gulp');
var gutil = require('gulp-util');
var path = require('path');
var fs = require('fs');
var del = require('del');

var mustache = require('gulp-mustache');

var concat = require('gulp-concat');

// we used the wrong variable here, in the next version we will remove SINGULARITY_BASE_URI
var serverBase = process.env.SINGULARITY_URI_BASE || process.env.SINGULARITY_BASE_URI || '/singularity';

var staticUri = process.env.SINGULARITY_STATIC_URI || (serverBase + '/static');
var appUri = process.env.SINGULARITY_APP_URI || (serverBase + '/ui');

var templateData = {
  staticRoot: staticUri,
  appRoot: appUri,
  apiRoot: process.env.SINGULARITY_API_URI || '',
  apiDocs: process.env.SINGULARITY_API_DOCS || 'http://getsingularity.com/Docs/reference/apidocs/api-index.html',
  slaveHttpPort: process.env.SINGULARITY_SLAVE_HTTP_PORT || 5051,
  title: process.env.SINGULARITY_TITLE || 'Singularity (local dev)',
  navColor: process.env.SINGULARITY_NAV_COLOR,
  defaultCpus: process.env.SINGUALRITY_DEFAULT_CPUS || 1,
  defaultMemory: process.env.SINGULARITY_DEFAULT_MEMORY || 128,
  defaultBounceExpirationMinutes: process.env.SINGULARITY_DEFAULT_BOUNCE_EXPIRATION_MINUTES || 60,
  defaultHealthcheckIntervalSeconds: process.env.SINGULARITY_DEFAULT_HEALTHCHECK_INTERVAL_SECONDS || 5,
  defaultHealthcheckTimeoutSeconds: process.env.SINGULARITY_HEALTHCHECK_TIMEOUT_SECONDS || 5,
  defaultStartupTimeoutSeconds: process.env.SINGULARITY_DEFAULT_STARTUP_TIMEOUT_SECONDS || 60,
  defaultHealthcheckMaxRetries: process.env.SINGULARITY_HEALTHCHECK_MAX_RETRIES || 0,
  showTaskDiskResource: process.env.SINGULARITY_SHOW_TASK_DISK_RESOURCE || 'false',
  hideNewDeployButton: process.env.SINGULARITY_HIDE_NEW_DEPLOY_BUTTON || 'false',
  hideNewRequestButton: process.env.SINGULARITY_HIDE_NEW_REQUEST_BUTTON || 'false',
  loadBalancingEnabled: process.env.SINGULARITY_LOAD_BALANCING_ENABLED || 'false',
  runningTaskLogPath: process.env.SINGULARITY_RUNNING_TASK_LOG_PATH || 'stdout',
  finishedTaskLogPath: process.env.SINGULARITY_FINISHED_TASK_LOG_PATH || 'stdout',
  commonHostnameSuffixToOmit: process.env.SINGULARITY_COMMON_HOSTNAME_SUFFIX_TO_OMIT || '',
  taskS3LogOmitPrefix: process.env.SINGULARITY_TASK_S3_LOG_OMIT_PREFIX || '',
  warnIfScheduledJobIsRunningPastNextRunPct: process.env.SINGULARITY_WARN_IF_SCHEDULED_JOB_IS_RUNNING_PAST_NEXT_RUN_PCT || 200,
  shellCommands: process.env.SINGULARITY_SHELL_COMMANDS || '[]',
  shortenSlaveUsageHostname: process.env.SINGULARITY_SHORTEN_SLAVE_USAGE_HOSTNAME || 'false',
  timestampFormat: process.env.SINGULARITY_TIMESTAMP_FORMAT || 'lll',
  timestampWithSecondsFormat: process.env.SINGULARITY_TIMESTAMP_WITH_SECONDS_FORMAT || 'lll:ss',
  redirectOnUnauthorizedUrl: process.env.SINGULARITY_REDIRECT_ON_UNAUTHORIZED_URL || '',
  extraScript: process.env.SINGULARITY_EXTRA_SCRIPT || '',
  sentryDsn: process.env.SINGULARITY_SENTRY_DSN || '',
  generateAuthHeader: process.env.SINGULARITY_GENERATE_AUTH_HEADER || 'false',
  authTokenKey: process.env.SINGULARITY_AUTH_TOKEN_KEY || 'token',
  authCookieName: process.env.SINGULARITY_AUTH_COOKIE_NAME || '',
  quickLinks: process.env.SINGULARITY_QUICK_LINKS || '{}'
};

var dest = path.resolve(__dirname, 'dist');

var webpackStream = require('webpack-stream');
var webpack = require('webpack');

var port = process.env.PORT || 3334;
var useHMR = process.env.USE_HMR || true;
var webpackHMRPath = serverBase + '/__webpack_hmr';

__webpack_public_path__ = serverBase;

gulp.task('clean', function() {
  return del(dest + '/*');
});

gulp.task('static', ['clean'], function() {
  return gulp.src(['app/assets/static/**/*'])
    .pipe(gulp.dest(dest + '/static'));
})

gulp.task('html', ['static'], function () {
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest));
});

gulp.task('debug-html', ['static'], function () {
  templateData.isDebug = true;
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest));
});

gulp.task('build', ['html'], function () {
  return gulp.src('app')
    .pipe(webpackStream(require('./webpack.config')))
    .pipe(gulp.dest(dest + '/static'));
});

gulp.task('serve', ['debug-html'], function () {
  var count = 0;
  var webpackConfig = require('./make-webpack-config')({
    isDebug: true,
    useHMR: useHMR,
    webpackHMRPath: webpackHMRPath,
    publicPath: staticUri
  });
  return new Promise(resolve => {
    var bs = require('browser-sync').create();
    var compiler = webpack(webpackConfig);
    // Node.js middleware that compiles application in watch mode with HMR support
    // http://webpack.github.io/docs/webpack-dev-middleware.html
    var webpackDevMiddleware = require('webpack-dev-middleware')(compiler, {
      publicPath: staticUri,
      stats: webpackConfig.stats,
    });
    var webpackHotMiddleware = require('webpack-hot-middleware')(compiler, {
      path: webpackHMRPath
    });
    compiler.plugin('done', function () {
      // Launch Browsersync after the initial bundling is complete
      if (++count === 1) {
        bs.init({
          port: port,
          startPath: appUri,
          open: false,
          socket: {
            domain: 'localhost:' + port,
            clientPath: '/singularity/browser-sync'
          },
          server: {
            baseDir: 'dist',
            middleware: [
              webpackDevMiddleware,
              webpackHotMiddleware,
              // Serve index.html for all unknown requests
              function(req, res, next) {
                if (req.headers.accept && req.headers.accept.startsWith('text/html')) {
                  req.url = '/index.html'; // eslint-disable-line no-param-reassign
                }
                next();
              },
            ],
          },
        }, resolve);
      }
    });
  });
});

gulp.task('default', ['build']);
