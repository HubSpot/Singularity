var webpack = require('webpack');
var WebpackDevServer = require('webpack-dev-server');
var config = require('./webpack.config');
var path = require('path');

var SINGULARITY_API_ROOT = process.env.SINGULARITY_API_ROOT;

var server = new WebpackDevServer(webpack(config), {
  publicPath: config.output.publicPath,
  hot: true,
  historyApiFallback: {
    rewrites: [
      {
        from: /^\/(?!singularity\/api|static).*$/,
        to: function() {
          return 'index.html';
        }
      }
    ]
  },
  proxy: {
    '/singularity/api/*': {
      target: SINGULARITY_API_ROOT,
      rewrite: function(req) {
        req.url = req.url.replace(/^\/singularity\/api/, '');
      }
    }
  }
});

// server.use('/singularity/api', function(req, res) {
//   res.sendFile(path.join(__dirname + '/index.html'));
// });

server.listen(3223, 'localhost', function (err, result) {
  if (err) {
    return console.log(err);
  }

  console.log('Listening at http://localhost:3223/');
});
