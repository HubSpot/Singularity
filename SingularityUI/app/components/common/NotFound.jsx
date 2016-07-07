import React from 'react';
import rootComponent from '../../rootComponent';

class NotFound extends React.Component {

  render() {
    return (
      <div>
        <div className="row text-center">
          <h1>Not found</h1>
          <h4>The page you are looking for doesn't exist:</h4>
          <code>{this.props.location.pathname}</code>
        </div>
        <div className="row text-center">
          <h4><a href={config.appRoot}>{"Go home"}</a></h4>
        </div>
      </div>
    );
  }
}

NotFound.propTypes = {
  location: React.PropTypes.object
};

export default rootComponent('Not Found', null, NotFound);
