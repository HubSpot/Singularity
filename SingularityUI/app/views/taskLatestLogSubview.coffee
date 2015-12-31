View = require './view'

class taskLatestLogSubview extends View

  initialize: ({@task, @logDir, @template}) ->
      @listenTo @task, 'change', @render
      @listenTo @task, 'sync', @render
      @listenTo @logDir, 'change', @render
      @listenTo @logDir, 'sync', @render

  render: =>
      return if not @task.synced or not @logDir.synced
      @$el.html @template @renderData()

  renderData: =>
      file = if @task.get('isStillRunning') then config.runningTaskLogPath else config.finishedTaskLogPath
      file = _.last(file.split('/'))
      exists = _.contains(_.pluck(@logDir.toJSON(), 'uiPath'), file)

      data: @task.toJSON()
      fileExists: exists
      config: config

module.exports = taskLatestLogSubview
