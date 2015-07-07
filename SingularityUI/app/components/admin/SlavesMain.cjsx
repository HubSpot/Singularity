Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

SectionHeader = require '../lib/SectionHeader'
PageHeader    = require '../lib/PageHeader'
SlavesTable   = require './SlavesTable'


SlavesMain = React.createClass
  
  displayName: 'Slaves Main'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions:  React.PropTypes.func.isRequired

  ##
  ## Build Tables
  ##
  render: ->
      <div>
        <PageHeader title="Slaves" titleCase=true />
        <Row id='active'>
          <Col md={12}>
            <SectionHeader title='Active' />
            <SlavesTable
              state='active'
              items={@props.data.active} 
              actions={@props.actions}
            />
          </Col>
        </Row>
        <Row id='decommission'>
          <Col md={12}>
            <SectionHeader title='Decommissioning' />
            <SlavesTable
              state='decommission'
              items={@props.data.decomm} 
              actions={@props.actions}
            />
          </Col>
        </Row>
        <Row id='inactive'>
          <Col md={12}>
            <SectionHeader title='Inactive' />
            <SlavesTable
              state='inactive'
              items={@props.data.inactive} 
              actions={@props.actions}
            />
          </Col>
        </Row>
      </div>
    
module.exports = SlavesMain