Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

SectionHeader = require '../lib/SectionHeader'
PageHeader    = require '../lib/PageHeader'
RacksTable   = require './RacksTable'


RacksMain = React.createClass
  
  displayName: 'Racks Main'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions:  React.PropTypes.func.isRequired

  ##
  ## Build Tables
  ##
  render: ->
    return (
      <div>
        <PageHeader title="Racks" titleCase=true />
        
        <Row id='active'>
          <Col md={12}>
            <SectionHeader title='Active' />
            <RacksTable
              state='active'
              items={@props.data.active} 
              actions={@props.actions}
            />
          </Col>
        </Row>

        <Row id='decommission'>
          <Col md={12}>
            <SectionHeader title='Decommissioning' />
            <RacksTable
              state='decommission'
              items={@props.data.decomm} 
              actions={@props.actions}
            />
          </Col>
        </Row>

        <Row id='inactive'>
          <Col md={12}>
            <SectionHeader title='Inactive' />
            <RacksTable
              state='inactive'
              items={@props.data.inactive} 
              actions={@props.actions}
            />
          </Col>
        </Row>

      </div>
    )

module.exports = RacksMain