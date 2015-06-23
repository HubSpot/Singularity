SectionLoadeer = React.createClass
  
  displayName: 'SectionLoader'

  propTypes:
    size: React.PropTypes.string

  render: ->
    <div className='page-loader centered cushy'></div>

module.exports = SectionLoadeer