React = require 'react'
classNames = require 'classnames'

LinkedFormItem = React.createClass

    render: ->
        FormItem1Class = @props.prop.formItem1.component
        FormItem2Class = @props.prop.formItem2.component
        <div id=@props.id className={classNames @props.prop.customClass, 'text-center'}>
            <FormItem1Class
                id = @props.prop.formItem1.id
                prop = @props.prop.formItem1.prop />
            {@props.prop.separator}
            <FormItem2Class
                id = @props.prop.formItem2.id
                prop = @props.prop.formItem2.prop />
        </div>

module.exports = LinkedFormItem
