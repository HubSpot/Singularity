import React from 'react';

const NotFound = (props) => {
  return (
    <div>
      <div className="row text-center">
        <h1>Not found</h1>
        <h4>The page you are looking for doesn't exist:</h4>
        <code>{props.location.pathname}</code>
      </div>
      <div className="row text-center">
        <h4><a href={config.appRoot}>{"Go home"}</a></h4>
      </div>
    </div>
  );
};

NotFound.propTypes = {
  location: React.PropTypes.object
};

export default NotFound;
