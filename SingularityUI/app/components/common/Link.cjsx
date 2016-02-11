Link = React.createClass

    render: ->
        <span title={@props.prop.altText}>
            <a href={@props.prop.url}>
                {@props.prop.text}
            </a>
        </span>

module.exports = Link
