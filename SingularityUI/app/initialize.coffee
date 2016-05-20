# Object.assign polyfill
require('es6-object-assign').polyfill()

# Promise polyfill
window.Promise = require 'promise-polyfill'

# Set up the only app globals
window.utils = require 'utils'
window.app = require 'application'

Messenger = require 'messenger'

require 'bootstrap'

vex = require 'vex.dialog'

apiRootPromptTemplate = require './templates/vex/apiRootPrompt'

# Set up third party configurations
require 'thirdPartyConfigurations'
# Set up the Handlebars helpers
require 'handlebarsHelpers'

# Initialize the app on DOMContentReady
$ ->
	if config.apiRoot
		app.initialize()
	else
		# In the event that the apiRoot isn't set (running locally)
		# prompt the user for it and refresh
		vex.dialog.prompt
			message: apiRootPromptTemplate()
			callback: (value) =>
				if value
					localStorage.setItem "apiRootOverride", value
				window.location = window.location.href
