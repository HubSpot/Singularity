Utils = require '../../utils'

Enums = require './Enums'

TaskSearch = React.createClass

    headerText: 'Search for Tasks'

    attributeOrEmptyString: (attr) ->
        if attr
            return attr
        else
            return ''

    getInitialState: ->
        return {
            blah: 'Blah!'
            text: 'Hello World!!!'
            requestId: @attributeOrEmptyString(@props.requestId)
        }

    handleSubmit: (event) ->
        @setState {
            blah: 'You hit the button!'
        }

    makeFormField: (title, inputType, changeFn, valueAttr) ->
        return <tr>
                    <th><b> {title} </b> </th>
                    <th><input type={inputType} onChange={changeFn} value={valueAttr} size=50 /></th>
                </tr>

    makeDropDown: (enumeration, forceChooseValue, title, inputType, changeFn, valueAttr) ->
        dropDownOpts = []
        if not forceChooseValue
            dropDownOpts.push(<option key=0 value='noValueChosen'>Any</option>)
        i = 1
        for element in enumeration
            dropDownOpts.push(<option key={i} value={element.value}>{element.user}</option>)
            i++
        return <tr>
                    <th><b> {title} </b></th>
                    <th><select type={inputType} onChange={changeFn} value={this.state.sortDirection}>
                        {dropDownOpts}
                    </select></th>
                </tr>

    onChangeRequestId: (event) ->
        if @props.requestLocked
            return @state
        @setState {
            requestId: event.target.value
        }

    render: ->
        <div>
            <h2> {@headerText} </h2>
            <form onSubmit={@handleSubmit}>
                <table ><tbody>
                    {@makeFormField 'Request ID', 'requestId', @onChangeRequestId, @state.requestId}
                    {@makeFormField 'Deploy ID', 'deployId', @onChangeDeployID, @state.deployId}
                    {@makeFormField 'Host', 'host', @onChangeHost, @state.host}
                    {@makeDropDown Enums.extendedTaskState(), false, 'Last Task Status', 'lastTaskStatus', @handleChangeLastTaskStatus, @state.lastTaskStatus}
                    {@makeFormField 'Started Before', 'startedBefore', @onChangeStartedBefore, @state.startedBefore}
                    {@makeFormField 'Started After', 'startedAfter', @onChangeStartedAfter, @state.startedAfter}
                    {@makeDropDown Enums.sortDirections(), true, 'Sort Direction', 'sortDirection', @handleChangeSortDirection, @state.sortDirection}
                </tbody></table>
                <button>Search</button>
            </form>
        </div>

module.exports = TaskSearch