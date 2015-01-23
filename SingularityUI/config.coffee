path = require 'path'
fs =   require 'fs'

handlebars = require 'handlebars-brunch/node_modules/handlebars'

# Brunch settings
exports.config =
    paths:
        public: path.resolve(__dirname, '../SingularityService/target/generated-resources/static')

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

    # Include full compiler to allow for compiling templates during runtime
    plugins:
        handlebars:
            include:
                runtime: false


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
            slaveHttpPort: 5051
            title: 'Singularity (local dev)'
            navColor: ''
            defaultCpus: 1
            defaultMemory: 128
            hideNewDeployButton: "false"
            hideNewRequestButton: "false"
            
        compiledTemplate = handlebars.compile(indexTemplate)(templateData)
        fs.writeFileSync destination, compiledTemplate
