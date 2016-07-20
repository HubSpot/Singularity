var gulp = require('gulp');
var gutil = require('gulp-util');
var path = require('path');
var fs = require('fs');
var del = require('del');

var mustache = require('gulp-mustache');

var concat = require('gulp-concat');

// we used the wrong variable here, in the next version we will remove SINGULARITY_BASE_URI
var serverBase = process.env.SINGULARITY_URI_BASE || process.env.SINGULARITY_BASE_URI || '/singularity';

var templateData = {
  staticRoot: process.env.SINGULARITY_STATIC_URI || (serverBase + '/static'),
  appRoot: process.env.SINGULARITY_APP_URI || (serverBase + '/ui'),
  apiRoot: process.env.SINGULARITY_API_URI || '',
  slaveHttpPort: process.env.SINGULARITY_SLAVE_HTTP_PORT || 5051,
  title: process.env.SINGULARITY_TITLE || 'Singularity (local dev)',
  navColor: process.env.SINGULARITY_NAV_COLOR,
  defaultCpus: process.env.SINGUALRITY_DEFAULT_CPUS || 1,
  defaultMemory: process.env.SINGULARITY_DEFAULT_MEMORY || 128,
  defaultBounceExpirationMinutes: process.env.SINGULARITY_DEFAULT_BOUNCE_EXPIRATION_MINUTES || 60,
  defaultHealthcheckIntervalSeconds: process.env.SINGULARITY_DEFAULT_HEALTHCHECK_INTERVAL_SECONDS || 5,
  defaultHealthcheckTimeoutSeconds: process.env.SINGULARITY_HEALTHCHECK_TIMEOUT_SECONDS || 5,
  defaultDeployHealthTimeoutSeconds: process.env.SINGULARITY_DEPLOY_HEALTH_TIMEOUT_SECONDS || 120,
  defaultHealthcheckMaxRetries: process.env.SINGULARITY_HEALTHCHECK_MAX_RETRIES || 0,
  hideNewDeployButton: process.env.SINGULARITY_HIDE_NEW_DEPLOY_BUTTON || 'false',
  hideNewRequestButton: process.env.SINGULARITY_HIDE_NEW_REQUEST_BUTTON || 'false',
  loadBalancingEnabled: process.env.SINGULARITY_LOAD_BALANCING_ENABLED || 'false',
  runningTaskLogPath: process.env.SINGULARITY_RUNNING_TASK_LOG_PATH || 'stdout',
  finishedTaskLogPath: process.env.SINGULARITY_FINISHED_TASK_LOG_PATH || 'stdout',
  commonHostnameSuffixToOmit: process.env.SINGULARITY_COMMON_HOSTNAME_SUFFIX_TO_OMIT || '',
  taskS3LogOmitPrefix: process.env.SINGULARITY_TASK_S3_LOG_OMIT_PREFIX || '',
  warnIfScheduledJobIsRunningPastNextRunPct: process.env.SINGULARITY_WARN_IF_SCHEDULED_JOB_IS_RUNNING_PAST_NEXT_RUN_PCT || 200,
  shellCommands: process.env.SINGULARITY_SHELL_COMMANDS || '[]',
  timestampFormat: process.env.SINGULARITY_TIMESTAMP_FORMAT || 'lll',
  timestampWithSecondsFormat: process.env.SINGULARITY_TIMESTAMP_WITH_SECONDS_FORMAT || 'lll:ss',
  redirectOnUnauthorizedUrl: process.env.SINGULARITY_REDIRECT_ON_UNAUTHORIZED_URL || ''
};

var dest = path.resolve(__dirname, 'dist');

var webpackStream = require('webpack-stream');
var webpack = require('webpack');

gulp.task('clean', function() {
  return del(dest + '/*');
});

gulp.task('html', function () {
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest));
});

gulp.task('debug-html', function () {
  templateData.isDebug = true;
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest));
});

gulp.task('build', ['clean', 'html'], function () {
  var prodWebpackConfig = require('./webpack.config.prod');
  return gulp.src('app')
    .pipe(webpackStream(prodWebpackConfig))
    .pipe(gulp.dest(dest + '/static'));
});

gulp.task('serve', ['clean', 'debug-html'], function () {
  var count = 0;
  var webpackConfig = require('./webpack.config.dev');
  return new Promise(resolve => {
    var bs = require('browser-sync').create();
    var compiler = webpack(webpackConfig);
    // Node.js middleware that compiles application in watch mode with HMR support
    // http://webpack.github.io/docs/webpack-dev-middleware.html
    var webpackDevMiddleware = require('webpack-dev-middleware')(compiler, {
      publicPath: webpackConfig.output.publicPath,
      stats: webpackConfig.stats,
    });
    var webpackHotMiddleware = require('webpack-hot-middleware')(compiler);
    compiler.plugin('done', function () {
      // Launch Browsersync after the initial bundling is complete
      if (++count === 1) {
        bs.init({
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
