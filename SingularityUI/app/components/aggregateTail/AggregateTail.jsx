'use strict';


module.exports = React.createClass({
  componentWillMount () {
    // backboneReact.on(this, {
    //   collections: {
    //     myCollection: collection1
    //   }
    // });
  },

  componentWillUnmount () {
    // backboneReact.off(this);
  },

  render () {
    return (
      <div>
        {this.props.text}
      </div>
    );
  }
});
