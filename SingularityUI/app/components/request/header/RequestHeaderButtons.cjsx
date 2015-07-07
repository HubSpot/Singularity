Helpers = require '../../utils/helpers'
RequestHeaderButton = require './RequestHeaderButton'


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

  getButtons: ->
    request = @props.data.request
    id = request.id
    deployLink = "#{config.appRoot}/request/#{request.id}/deploy"

    buttonComponents = []

    generateButtonKey = ->
      buttonComponents.length + 1

    buttonComponents.push(
      <RequestHeaderButton key={generateButtonKey()} buttonClick={@viewJSON} bsStyle='default'>
        JSON
      </RequestHeaderButton>
    )

    if not config.hideNewDeployButton
      buttonComponents.push(
        <RequestHeaderButton key={generateButtonKey()} bsStyle='success' id={id} link={deployLink}>
          Deploy
        </RequestHeaderButton>
      )

    if request.canBeRunNow and not request.deleted
      buttonComponents.push(
        <RequestHeaderButton key={generateButtonKey()} buttonClick={@requestAction} action='promptRun' bsStyle='primary'>
          Run now
        </RequestHeaderButton>
      )

    if request.canBeScaled
      buttonComponents.push(
        <RequestHeaderButton key={generateButtonKey()} buttonClick={@requestAction} action='promptScale' bsStyle='primary'>
          Scale 
        </RequestHeaderButton>
      )

    unless request.deleted and data.paused
      buttonComponents.push(
        <RequestHeaderButton key={generateButtonKey()} buttonClick={@requestAction} action='promptUnpause' bsStyle='primary'>
          Unpause
        </RequestHeaderButton>
      )

    unless request.deleted and not data.paused
      buttonComponents.push(
        <RequestHeaderButton key={generateButtonKey()} buttonClick={@requestAction} action='promptPause' bsStyle='primary'>
          Pause
        </RequestHeaderButton>
      )
    
    if request.canBeBounced
      buttonComponents.push(
        <RequestHeaderButton key={generateButtonKey()} buttonClick={@requestAction} action='promptBounce' bsStyle='primary'>
          Bounce
        </RequestHeaderButton>
      )
      
    unless request.deleted
      buttonComponents.push(
        <RequestHeaderButton key={generateButtonKey()} buttonClick={@remove} bsStyle='danger'>
          Remove
        </RequestHeaderButton>
      )

    return buttonComponents

  render: ->
    <div>
      {@getButtons()}
    </div>

module.exports = RequestHeaderButtons