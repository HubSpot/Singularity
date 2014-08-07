Model = require './model'

class LogLine extends Model
    
    idAttribute: 'offset'

    getStartOffset: =>
    	@get('offset')

    getEndOffset: =>
    	@get('offset') + @get('data').length

module.exports = LogLine