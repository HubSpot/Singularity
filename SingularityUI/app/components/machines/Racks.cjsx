React = require 'react'
MachinesPage = require './MachinesPage'
PlainText = require '../common/atomicDisplayItems/PlainText'
TimeStamp = require '../common/atomicDisplayItems/TimeStamp'
Link = require '../common/atomicDisplayItems/Link'
Glyphicon = require '../common/atomicDisplayItems/Glyphicon'
Utils = require '../../utils'

Racks = React.createClass

    typeName: {
        'active': 'Activated By'
        'frozen': 'Frozen By'
        'decommissioning': 'Decommissioned By'
    }

    showUser: (rack) ->
        rack.state in ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']

    columnHeads: (type) ->
        heads = [
            {
                data: 'ID'
            },
            {
                data: 'Current State'
            },
            {
                data: 'Uptime'
            }
        ]
        if @typeName[type]
            heads.push {
                data: @typeName[type]
            }
        heads.push { data: 'Message' }
        heads.push {} # Reactivate button and Decommission or Remove button
        heads

    refresh: () -> @props.refresh()

    promptReactivate: (event, rackModel) ->
        event.preventDefault()
        rackModel.promptReactivate () => @refresh

    promptDecommission: (event, rackModel) ->
        event.preventDefault()
        rackModel.promptDecommission () => @refresh

    promptRemove: (event, rackModel) ->
        event.preventDefault()
        rackModel.promptRemove () => @refresh

    getMaybeReactivateButton: (rackModel) ->
        rack = rackModel.attributes
        if rack.state in ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'new-window'
                    />
                    onClickFn: (event) => @promptReactivate event, rackModel
                    title: 'Reactivate'
                    altText: "Reactivate #{rack.id}"
                    overlayTrigger: true
                    overlayTriggerPlacement: 'top'
                    overlayToolTipContent: "Reactivate #{rack.id}"
                    overlayId: "reactivate#{rack.id}"
                }}
            />
        else
            return null

    getDecommissionOrRemoveButton: (rackModel) ->
        rack = rackModel.attributes
        if rack.state is 'ACTIVE'
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'trash'
                    />
                    onClickFn: (event) => @promptDecommission event, rackModel
                    title: 'Decommission'
                    altText: "Decommission #{rack.id}"
                    overlayTrigger: true
                    overlayTriggerPlacement: 'top'
                    overlayToolTipContent: "Decommission #{rack.id}"
                    overlayId: "decommission#{rack.id}"
                }}
            />
        else
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'remove'
                    />
                    onClickFn: (event) => @promptRemove event, rackModel
                    title: 'Remove'
                    altText: "Remove #{rack.id}"
                    overlayTrigger: true
                    overlayTriggerPlacement: 'top'
                    overlayToolTipContent: "Remove #{rack.id}"
                    overlayId: "remove#{rack.id}"
                }}
            />

    getData: (type, rackModel) ->
        rack = rackModel.attributes
        data = [
            {
                component: PlainText
                prop: {
                    text: rack.id
                }
            },
            {
                component: PlainText
                prop: {
                    text: Utils.humanizeText rack.state
                }
            },
            {
                component: TimeStamp
                prop: {
                    display: 'duration'
                    timestamp: rack.uptime
                }
            }
        ]
        if @typeName[type]
            data.push {
                component: PlainText
                prop: {
                    text: if @showUser(rack) and rack.user then rack.user else ''
                }
            }
        data.push {
            component: PlainText
            prop: {
                text: rack.currentState.message or ''
            }
        }
        data.push {
            component: PlainText
            className: 'actions-column'
            prop: {
                text: <div>{@getMaybeReactivateButton rackModel} {@getDecommissionOrRemoveButton rackModel} </div>
            }
        }
        data

    getRacks: (type, racks) ->
        tableifiedRacks = []
        racks.map (rack) =>
            tableifiedRacks.push {
                dataId: rack.id
                data: @getData type, rack
            }
        tableifiedRacks

    getStates: ->
        [
            {
                stateName: "Active"
                emptyTableMessage: "No Active Racks"
                stateTableColumnMetadata: @columnHeads 'active'
                hostsInState: @getRacks 'active', @props.activeRacks
            },
            {
                stateName: "Decommissioning"
                emptyTableMessage: "No Decommissioning Racks"
                stateTableColumnMetadata: @columnHeads 'decommissioning'
                hostsInState: @getRacks 'decommissioning', @props.decommissioningRacks
            },
            {
                stateName: "Inactive"
                emptyTableMessage: "No Inactive Racks"
                stateTableColumnMetadata: @columnHeads 'inactive'
                hostsInState: @getRacks 'inactive', @props.inactiveRacks
            }
        ]

    render: ->
        <MachinesPage 
            header = "Racks"
            states = {@getStates()}
        />

module.exports = Racks
