Link = React.createClass

    render: ->
        <a href={@props.prop.url} title={@props.prop.title} onClick={@props.prop.onClickFn} className={@props.prop.className} key={@props.key}>
            {@props.prop.text}
        </a>

module.exports = Link
