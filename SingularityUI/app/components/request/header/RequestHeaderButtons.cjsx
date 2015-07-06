Helpers = require '../../utils/helpers'
RequestButton = require './RequestButton'


RequestHeaderButtons = React.createClass

  displayName: 'RequestHeaderButtons'

  propTypes:
    data: React.PropTypes.object.isRequired
    refresh: React.PropTypes.func.isRequired

  viewJSON: (e) ->
    utils.viewJSON @props.data.requestModel

  requestAction: (e) ->
    method = e.currentTarget.getAttribute('data-action')
    @props.data.requestModel[method] =>
      @props.refresh()

  remove: (e) ->
    @props.data.requestModel.promptRemove =>
      Helpers.routeComponentLink null, 'requests', true

  run: (e) ->
    @props.data.requestModel.promptRun (data) =>
      ## TO DO:
      ## Confirm that this works...
      # If user wants to redirect to a file after the task starts
      if data.autoTail is 'on'
        autoTailer = new @props.AutoTailer({
          requestId: @props.data.request.id
          autoTailFilename: data.filename
          autoTailTimestamp: +new Date()
        })
        autoTailer.startAutoTailPolling()
      else
        @props.refresh()
        setTimeout ( => @props.refresh() ), 2500

  render: ->
    request = @props.data.request
    id = request.id
    deployLink = "#{config.appRoot}/request/#{request.id}/deploy"

    SPACE = ' '
    <div>
      <RequestButton buttonClick={@viewJSON} bsStyle='default'>
        JSON
      </RequestButton>
      {SPACE}
      <RequestButton buttonClick={@requestAction} action='promptScale' bsStyle='primary'>
        Scale
      </RequestButton>
      {SPACE}
      <RequestButton buttonClick={@requestAction} action='promptPause' bsStyle='primary'>
        Pause
      </RequestButton>
      {SPACE}
      <RequestButton buttonClick={@requestAction} action='promptBounce' bsStyle='primary'>
        Bounce
      </RequestButton>
      {SPACE}
      <RequestButton buttonClick={@remove} bsStyle='danger'>
        Remove
      </RequestButton>
      {SPACE}
      <RequestButton bsStyle='success' id={id} link={deployLink}>
        Deploy
      </RequestButton>
      {SPACE}
      <RequestButton buttonClick={@requestAction} action='promptRun' bsStyle='primary'>
        Run now
      </RequestButton>
      {SPACE}
      <RequestButton buttonClick={@requestAction} action='promptUnpause' bsStyle='primary'>
        Unpause
      </RequestButton>
    </div>

module.exports = RequestHeaderButtons