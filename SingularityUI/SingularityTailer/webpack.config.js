var path = require('path');
var webpack = require('webpack');

module.exports = {
  devtool: 'source-map',
  entry: {
    'tailer': [
      'react-hot-loader/patch',
      'webpack-dev-server/client?http://localhost:3223',
      'webpack/hot/only-dev-server',
      './example/index',
    ]
  },
  output: {
    path: path.join(__dirname, 'dist'),
    filename: 'bundle.js',
    publicPath: '/static/',
  },
  plugins: [
    new webpack.HotModuleReplacementPlugin()
  ],
  module: {
    loaders: [
      {
        test: /\.js$/,
        loaders: ['babel'],
        exclude: /node_modules/,
      },
      {
        test: /\.test\.js$/,
        loaders: ['mocha', 'babel'],
        exclude: /node_modules/,
      },
      {
        test: /\.css$/,
        loader: 'style-loader!css-loader',
      },
      {
        test: /\.scss$/,
        loader: 'style-loader?sourceMap!css-loader?sourceMap!resolve-url?fail!sass?sourceMap',
      },
    ],
  },
};
