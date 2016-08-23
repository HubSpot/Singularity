var webpack = require('webpack');
var CaseSensitivePathsPlugin = require('case-sensitive-paths-webpack-plugin');
var path = require('path');

var dest = path.resolve(__dirname, 'dist');

module.exports = {
  entry: {
    app: './app/initialize.jsx',
    vendor: [
      'react',
      'jquery',
      'underscore',
      'clipboard',
      'select2',
      'moment',
      'messenger',
      'bootstrap',
      'classnames',
      'react-interval',
      'react-dom',
      'fuzzy',
      'juration',
      'vex-js'
    ],
  },
  output: {
    path: dest,
    filename: 'app.js'
  },
  debug: true,
  devtool: 'source-map',
  module: {
    loaders: [
      { test: /\.es6$/, exclude: /node_modules/, loader: 'babel-loader' },
      { test: /\.jsx$/, exclude: /node_modules/, loader: 'babel-loader' },
      { test: /[\/]messenger\.js$/, loader: 'exports?Messenger'}
    ]
  },
  resolve: {
    root: path.resolve('./app'),
    extensions: ['', '.js', '.es6', '.jsx'],
    alias: {
      'vex': 'vex-js/js/vex.js',
      'vex.dialog': 'vex-helper.es6',
      'bootstrap': 'bootstrap/dist/js/bootstrap.js'
    }
  },
  plugins: [
    new webpack.ProvidePlugin({
      $: 'jquery',
      '_': 'underscore',
      jQuery: 'jquery',
      'window.jQuery': 'jquery'
    }),
    new webpack.optimize.CommonsChunkPlugin('vendor', 'vendor.bundle.js'),
    new CaseSensitivePathsPlugin()
  ]
};
