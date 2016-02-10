Link = React.createClass

    render: ->
        <span title={@props.altText}>
            <a href={@props.url}>
                {@props.text}
            </a>
        </span>

module.exports = Link