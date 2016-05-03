React = require 'react'
MachinesPage = require './MachinesPage'
PlainText = require '../common/atomicDisplayItems/PlainText'
TimeStamp = require '../common/atomicDisplayItems/TimeStamp'
Link = require '../common/atomicDisplayItems/Link'
Glyphicon = require '../common/atomicDisplayItems/Glyphicon'
Utils = require '../../utils'
SlavesCollection = require '../../collections/Slaves'

Slaves = React.createClass

    typeName: {
        'active': 'Activated By'
        'frozen': 'Frozen By'
        'decommissioning': 'Decommissioned By'
    }

    showUser: (slave) ->
        slave.state in ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']

    columnHeads: (type) ->
        heads = [
            {
                data: 'ID'
            },
            {
                data: 'State'
            },
            {
                data: 'Since'
            },
            {
                data: 'Rack'
            },
            {
                data: 'Host'
            },
            {
                data: 'Uptime'
                className: 'hidden-xs'
            }
        ]
        if @typeName[type]
            heads.push {
                data: @typeName[type]
            }
        heads.push { data: 'Message' }
        heads.push {} # Reactivate button and Decommission or Remove button
        heads

    refresh: () -> @props.slaves.fetch().done => @forceUpdate()

    promptReactivate: (event, slaveModel) ->
        event.preventDefault()
        slaveModel.promptReactivate () => @refresh()

    promptDecommission: (event, slaveModel) ->
        event.preventDefault()
        slaveModel.promptDecommission () => @refresh()

    promptFreeze: (event, slaveModel) ->
        event.preventDefault()
        slaveModel.promptFreeze () => @refresh()

    promptRemove: (event, slaveModel) ->
        event.preventDefault()
        slaveModel.promptRemove () => @refresh()

    getMaybeReactivateButton: (slaveModel) ->
        slave = slaveModel.attributes
        if slave.state in ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'new-window'
                    />
                    onClickFn: (event) => @promptReactivate event, slaveModel
                    title: 'Reactivate'
                    altText: "Reactivate #{slave.id}"
                    overlayTrigger: true
                    overlayTriggerPlacement: 'top'
                    overlayToolTipContent: "Reactivate #{slave.id}"
                    overlayId: "reactivate#{slave.id}"
                }}
            />
        else
            return null

    getMaybeFreezeButton: (slaveModel) ->
        slave = slaveModel.attributes
        if slave.state is 'ACTIVE'
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'stop'
                    />
                    onClickFn: (event) => @promptFreeze event, slaveModel
                    title: 'Freeze'
                    altText: "Freeze #{slave.id}"
                    overlayTrigger: true
                    overlayTriggerPlacement: 'top'
                    overlayToolTipContent: "Freeze #{slave.id}"
                    overlayId: "freeze#{slave.id}"
                }}
            />
        else
            return null

    getDecommissionOrRemoveButton: (slaveModel) ->
        slave = slaveModel.attributes
        if slave.state in ['ACTIVE', 'FROZEN']
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'trash'
                    />
                    onClickFn: (event) => @promptDecommission event, slaveModel
                    title: 'Decommission'
                    altText: "Decommission #{slave.id}"
                    overlayTrigger: true
                    overlayTriggerPlacement: 'top'
                    overlayToolTipContent: "Decommission #{slave.id}"
                    overlayId: "decommission#{slave.id}"
                }}
            />
        else
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'remove'
                    />
                    onClickFn: (event) => @promptRemove event, slaveModel
                    title: 'Remove'
                    altText: "Remove #{slave.id}"
                    overlayTrigger: true
                    overlayTriggerPlacement: 'top'
                    overlayToolTipContent: "Remove #{slave.id}"
                    overlayId: "remove#{slave.id}"
                }}
            />

    getData: (type, slaveModel) ->
        slave = slaveModel.attributes
        data = [
            {
                component: Link
                prop: {
                    text: slave.id
                    url: "#{config.appRoot}/tasks/active/all/#{slave.host}"
                    altText: "All tasks running on host #{slave.host}"
                }
            },
            {
                component: PlainText
                prop: {
                    text: Utils.humanizeText slave.state
                }
            },
            {
                component: TimeStamp
                prop: {
                    display: 'absoluteTimestamp'
                    timestamp: slave.currentState.timestamp
                }
            },
            {
                component: PlainText
                prop: {
                    text: slave.rackId
                }
            },
            {
                component: PlainText
                prop: {
                    text: slave.host
                }
            },
            {
                component: TimeStamp
                prop: {
                    display: 'duration'
                    timestamp: slave.uptime
                }
            }
        ]
        if @typeName[type]
            data.push {
                component: PlainText
                prop: {
                    text: if @showUser(slave) and slave.user then slave.user else ''
                }
            }
        data.push {
            component: PlainText
            prop: {
                text: slave.currentState.message or ''
            }
        }
        data.push {
            component: PlainText
            className: 'actions-column'
            prop: {
                text: <div>
                    {@getMaybeReactivateButton slaveModel}
                    {@getMaybeFreezeButton slaveModel}
                    {@getDecommissionOrRemoveButton slaveModel}
                </div>
            }
        }
        data

    getSlaves: (type, slaves) ->
        tableifiedSlaves = []
        slaves.map (slave) =>
            tableifiedSlaves.push {
                dataId: slave.id
                data: @getData type, slave
            }
        tableifiedSlaves

    getActiveSlaves: ->
        return new SlavesCollection(
            @props.slaves.filter (model) ->
                model.get('state') in ['ACTIVE']
        )

    getFrozenSlaves: ->
        return new SlavesCollection(
            @props.slaves.filter (model) ->
                model.get('state') in ['FROZEN']
        )

    getDecommissioningSlaves: ->
        return new SlavesCollection(
            @props.slaves.filter (model) ->
                model.get('state') in ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']
        )

    getInactiveSlaves: ->
        return new SlavesCollection(
            @props.slaves.filter (model) ->
                model.get('state') in ['DEAD', 'MISSING_ON_STARTUP']
        )

    getStates: ->
        [
            {
                stateName: "Active"
                emptyTableMessage: "No Active Slaves"
                stateTableColumnMetadata: @columnHeads 'active'
                hostsInState: @getSlaves 'active', @getActiveSlaves()
            },
            {
                stateName: "Frozen"
                emptyTableMessage: "No Frozen Slaves"
                stateTableColumnMetadata: @columnHeads 'frozen'
                hostsInState: @getSlaves 'decommissioning', @getFrozenSlaves()
            },
            {
                stateName: "Decommissioning"
                emptyTableMessage: "No Decommissioning Slaves"
                stateTableColumnMetadata: @columnHeads 'decommissioning'
                hostsInState: @getSlaves 'decommissioning', @getDecommissioningSlaves()
            },
            {
                stateName: "Inactive"
                emptyTableMessage: "No Inactive Slaves"
                stateTableColumnMetadata: @columnHeads 'inactive'
                hostsInState: @getSlaves 'inactive', @getInactiveSlaves()
            }
        ]

    render: ->
        <MachinesPage 
            header = "Slaves"
            states = {@getStates()}
        />

module.exports = Slaves
