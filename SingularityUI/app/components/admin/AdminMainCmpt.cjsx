PageHeader = require '../lib/PageHeader'
ItemSection = require './ItemSection'

AdminMain = React.createClass

  displayName: 'AdminMain'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions:  React.PropTypes.func.isRequired

  render: ->

    <div>
        <PageHeader title={@props.label} titleCase=true />
        
        <ItemSection 
          title='Active'
          state='active'
          label={@props.label}
          items={@props.data.active} 
          actions={@props.actions}
        />

        <ItemSection 
          title='Decommissioning'
          state='decommission'
          label={@props.label}
          items={@props.data.decomm}
          actions={@props.actions}
        />

        <ItemSection 
          title='Inactive'
          state='inactive'
          label={@props.label}
          items={@props.data.inactive}
          actions={@props.actions}
        />
    </div>

module.exports = AdminMain