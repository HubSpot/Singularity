var path = require('path');
var extend = require('extend');
var webpack = require('webpack');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var CaseSensitivePathsPlugin = require('case-sensitive-paths-webpack-plugin');
var pkg = require('./package.json');

var dest = path.resolve(__dirname, 'dist/static');

module.exports = function(options) {
  var isDebug = options.isDebug;
  var isVerbose = options.isVerbose;
  var useHMR = options.useHMR;
  var webpackHMRPath = options.webpackHMRPath || '/__webpack_hmr';
  var publicPath = options.publicPath || '/static/';

  var extractCSS = new ExtractTextPlugin('css/app.css', {
    disable: isDebug
  });

  var config = {

    // The base directory for resolving the entry option
    context: __dirname,

    // The entry point for the bundle
    entry: {
      'js/app': (isDebug && useHMR)
        ? ['./app/initialize.jsx', 'react-hot-loader/patch', 'webpack-hot-middleware/client?path=' + webpackHMRPath]
        : './app/initialize.jsx',
      'js/vendor': [
        'react',
        'jquery',
        'underscore',
        'select2',
        'moment',
        'messenger',
        'bootstrap-sass',
        'classnames',
        'react-interval',
        'react-dom',
        'fuzzy',
        'juration',
      ],
    },

    devServer: {
      contentBase: './dist/static',
      hot: useHMR,
    },

    // Options affecting the output of the compilation
    output: {
      path: path.resolve(__dirname, dest),
      publicPath: publicPath,
      filename: '[name].bundle.js',
      chunkFilename: '[id].bundle.js?',
      sourcePrefix: '  ',
    },

    // Switch loaders to debug or release mode
    debug: isDebug,

    // Developer tool to enhance debugging, source maps
    // http://webpack.github.io/docs/configuration.html#devtool
    devtool: isDebug ? 'eval-source-map' : 'source-map',

    // What information should be printed to the console
    stats: {
      colors: true,
      reasons: isDebug,
      hash: isVerbose,
      version: isVerbose,
      timings: true,
      chunks: isVerbose,
      chunkModules: isVerbose,
      cached: isVerbose,
      cachedAssets: isVerbose,
    },

    // The list of plugins for Webpack compiler
    plugins: [
      new webpack.optimize.OccurrenceOrderPlugin(),
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': isDebug ? '"development"' : '"production"',
        __DEV__: isDebug,
      }),
      new webpack.ProvidePlugin({
        $: 'jquery',
        '_': 'underscore',
        jQuery: 'jquery',
        'window.jQuery': 'jquery'
      }),
      extractCSS,
      new CaseSensitivePathsPlugin()
    ],

    // Options affecting the normal modules
    module: {
      loaders: [
        {
          test: /(\.jsx?$|\.es6?$)/,
          include: [
            path.resolve(__dirname, './app'),
          ],
          loader: 'babel-loader',
          query: extend({}, pkg.babel, {
            cacheDirectory: useHMR,
          }),
        },
        {
          test: /\.scss/,
          loader: extractCSS.extract('style-loader?sourceMap', 'css-loader?sourceMap!resolve-url?fail!sass?sourceMap'),
        },
        {
          test: /\.styl/,
          loader: extractCSS.extract('style-loader?sourceMap', 'css-loader?sourceMap!stylus?sourceMap'),
        },
        {
          test: /\.css/,
          loader: extractCSS.extract('style-loader?sourceMap', 'css-loader?sourceMap'),
        },
        {
          test: /\.(png|jpg|jpeg|gif|ico)$/,
          loader: 'url-loader?limit=100000&name=images/[name].[ext]',
        },
        {
          test: /\.(eot|ttf|woff|woff2|svg)$/,
          loader: 'url-loader?limit=100000&fonts/[name].[ext]',
        },
        {
          test: /[\/]messenger\.js$/,
          loader: 'exports?Messenger',
        },
      ],
    },

    resolve: {
      root: [
        path.resolve('./app'),
        path.resolve('node_modules/bootstrap-sass/assets/fonts/bootstrap'),
      ],
      extensions: ['', '.js', '.es6', '.jsx', '.scss', '.styl', '.css'],
      alias: {
        'bootstrap': 'bootstrap/dist/js/bootstrap.js',
      },
    },

    sassLoader: {
      includePaths: [path.resolve(__dirname, './app/styles')],
    },

    stylus: {
      use: [require('nib')()],
      import: ['~nib/lib/nib/index.styl']
    }
  };

  // Optimize the bundle in release (production) mode
  if (!isDebug) {
    config.plugins.push(new webpack.optimize.DedupePlugin());
    config.plugins.push(new webpack.optimize.UglifyJsPlugin({ compress: { warnings: isVerbose } }));
    config.plugins.push(new webpack.optimize.AggressiveMergingPlugin());
    config.plugins.push(new webpack.optimize.CommonsChunkPlugin('js/vendor', 'js/vendor.bundle.js'));
  }

  // Hot Module Replacement (HMR) + React Hot Reload
  if (isDebug && useHMR) {
    config.plugins.push(new webpack.HotModuleReplacementPlugin());
    config.plugins.push(new webpack.NoErrorsPlugin());
  }

  return config;
};
