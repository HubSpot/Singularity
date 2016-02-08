Link = React.createClass

    getLink: ->
        return window.config.appRoot + "/task/" + @props.taskId

    render: ->
        <span title={@props.altText}>
            <a href={@props.url}>
                {@props.text}
            </a>
        </span>

module.exports = Link