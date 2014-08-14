path = require 'path'
fs =   require 'fs'

handlebars = require 'handlebars-brunch/node_modules/handlebars'

# Brunch settings
exports.config =
    paths:
        public: path.resolve(__dirname, '../SingularityService/src/main/resources/static')

    files:
        javascripts:
            joinTo: 'static/js/app.js'
            order: before: [
                /^(bower_components|vendor)/
            ]

        stylesheets:
            joinTo: 'static/css/app.css'

        templates:
            defaultExtension: 'hbs'
            joinTo: 'static/js/app.js'

    server:
        base: '/singularity'


    # When running SingularityUI via brunch server we need to make an index.html for it
    # based on the template that's shared with SingularityService
    # 
    # After we compile the static files, compile index.html using some required configs
    onCompile: =>
        destination = path.resolve @config.paths.public, 'index.html'

        templatePath = path.resolve 'app/assets/_index.mustache'
        indexTemplate = fs.readFileSync templatePath, 'utf-8'

        templateData =
            staticRoot: "#{ @config.server.base }/static"
            appRoot: @config.server.base
            apiRoot: ''
            mesosLogsPort: 5051
            title: 'Singularity (local dev)'

        compiledTemplate = handlebars.compile(indexTemplate)(templateData)
        fs.writeFileSync destination, compiledTemplate
