Model = require './model'

# Used to POST new delays by the deploy form
class AjaxError extends Model
  defaults:
    status: 0
    message: '(no message)'
    present: false
    shouldRefresh: false

  parse: (data) ->
    if data.responseText.match /Task .*? does not have a directory yet - check again soon/
      data.message = "Hang tight, this task is still starting up!"
    else
      data.message = data.responseText

    data.shouldRefresh = data.status is 400

module.exports = AjaxError
