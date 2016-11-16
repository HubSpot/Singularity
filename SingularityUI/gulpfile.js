var gulp = require('gulp');
var gutil = require('gulp-util');
var path = require('path');
var del = require('del');

var mustache = require('gulp-mustache');
var stylus = require('gulp-stylus');
var nib = require('nib');

var concat = require('gulp-concat');
var webpackMerge = require('webpack-merge');

var sass = require('gulp-sass');
var streamqueue = require('streamqueue');

var eslint = require('gulp-eslint');

// we used the wrong variable here, in the next version we will remove SINGULARITY_BASE_URI
var serverBase = process.env.SINGULARITY_URI_BASE || process.env.SINGULARITY_BASE_URI || '/singularity';

var templateData = {
  staticRoot: process.env.SINGULARITY_STATIC_URI || (serverBase + '/static'),
  appRoot: process.env.SINGULARITY_APP_URI || (serverBase + '/ui'),
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
  timestampFormat: process.env.SINGULARITY_TIMESTAMP_FORMAT || 'lll',
  timestampWithSecondsFormat: process.env.SINGULARITY_TIMESTAMP_WITH_SECONDS_FORMAT || 'lll:ss',
  redirectOnUnauthorizedUrl: process.env.SINGULARITY_REDIRECT_ON_UNAUTHORIZED_URL || '',
  extraScript: process.env.SINGULARITY_EXTRA_SCRIPT || ''
};

var dest = path.resolve(__dirname, 'dist');

var webpackStream = require('webpack-stream');
var webpack = require('webpack');
var webpackConfig = require('./webpack.config');
var WebpackDevServer = require('webpack-dev-server');

gulp.task('clean', function() {
  return del(dest);
});

gulp.task('fonts', function() {
  return gulp.src([
    './node_modules/bootstrap/dist/fonts/*.{eot,svg,ttf,woff,svg,woff2}'
  ]).pipe(gulp.dest(dest + '/static/fonts'));
});

gulp.task('scripts', function () {
  var prodConfig = Object.create(webpackConfig);

  prodConfig.plugins = prodConfig.plugins.concat(
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        warnings: false
      }
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('production')
    })
  );

  return gulp.src(prodConfig.entry.app)
    .pipe(webpackStream(prodConfig))
    .pipe(gulp.dest(dest + '/static/js'));
});

gulp.task('html', function () {
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest));
});

gulp.task('css-images', function () {
  return gulp.src('node_modules/select2/*.{gif,png}')
    .pipe(gulp.dest(dest + '/static/css'));
});

gulp.task('images', function () {
  return gulp.src('app/assets/static/images/*.ico')
    .pipe(gulp.dest(dest + '/static/images'));
});

gulp.task('styles', function () {
  var stylusStyles = gulp.src([
    'node_modules/vex-js/css/*.css',
    'node_modules/messenger/build/css/*.css',
    'node_modules/select2/*.css',
    'node_modules/bootstrap/dist/css/bootstrap.css',
    'node_modules/eonasdan-bootstrap-datetimepicker/build/css/bootstrap-datetimepicker.css',
    'node_modules/react-select/dist/react-select.css',
    'node_modules/react-tagsinput/react-tagsinput.css',
    'app/**/*.styl'
  ])
  .pipe(stylus({
    use: nib(),
    'include css': true
  }));

  var sassStyles = gulp.src('app/styles/scss/**/*.scss')
    .pipe(sass({errLogToConsole: true}));

  return streamqueue({ objectMode: true }, stylusStyles, sassStyles)
    .pipe(concat('app.css'))
    .pipe(gulp.dest(dest + '/static/css'));
});

gulp.task('lint', function () {
  return gulp.src(['./app/**/*.{es6, jsx}'])
    .pipe(eslint())
    .pipe(eslint.format());
});

gulp.task('build', ['clean'], function () {
  gulp.start(['scripts', 'html', 'styles', 'fonts', 'images', 'css-images', 'lint']);
});

gulp.task('serve', ['html', 'styles', 'fonts', 'images', 'css-images', 'lint'], function () {
  gulp.watch('app/**/*.styl', ['styles']);
  gulp.watch('app/**/*.scss', ['styles']);

  new WebpackDevServer(webpack(webpackMerge(webpackConfig, {devtool: 'eval'})), {
    contentBase: dest,
    historyApiFallback: true
  }).listen(3334, 'localhost', function (err) {
    if (err) throw new gutil.PluginError('webpack-dev-server', err);
    gutil.log('[webpack-dev-server]', 'Development server running on port 3334');
  });
});

gulp.task('default', ['build']);
