SectionHeader = React.createClass
  
  displayName: 'SectionHeader'

  render: ->
    <div className="section-header">
      <h2>{@props.title}</h2>
    </div>

module.exports = SectionHeader