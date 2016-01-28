Utils = require '../../utils'

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
                <table >
                    <tr>
                        <th><b> Request ID </b></th>
                        <th><input type='requestId' onChange={this.onChangeRequestId} value={this.state.requestId} size=50 /></th>
                    </tr>
                    <tr>
                        <th><b> Deploy ID </b></th>
                        <th><input type='deployId' onChange={this.onChange} value={this.state.deployId} size=50 /></th>
                    </tr>
                    <tr>
                        <th><b> Host </b></th>
                        <th><input type='host' onChange={this.onChange} value={this.state.host} size=50 /></th>
                    </tr>
                    <tr>
                        <th><b> Last Task Status </b></th>
                        <th><input type='lastTaskStatus' onChange={this.onChange} value={this.state.lastTaskStatus} size=50 /></th>
                    </tr>
                    <tr>
                        <th><b> Started Before </b></th>
                        <th><input type='startedBefore' onChange={this.onChange} value={this.state.startedBefore} size=50 /></th>
                    </tr>
                    <tr>
                        <th><b> Started After </b></th>
                        <th><input type='startedAfter' onChange={this.onChange} value={this.state.startedAfter} size=50 /></th>
                    </tr>
                    <tr>
                        <th><b> Sort Direction </b></th>
                        <th><select value={this.state.selectValue} onChange={this.handleChange}>
                                <option value="Ascending">Ascending</option>
                                <option value="Descending">Descending</option>
                        </select></th>
                    </tr>
                </table>
                <button>Search</button>
            </form>
        </div>

module.exports = TaskSearch