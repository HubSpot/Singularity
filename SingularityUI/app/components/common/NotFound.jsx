import React from 'react';
import rootComponent from '../../rootComponent';

import { Link } from 'react-router';

const NotFound = (props) => (
  <div>
    <div className="row text-center">
      <h1>Not found</h1>
      <h4>The page you are looking for doesn't exist:</h4>
      <code>{props.location.pathname}</code>
    </div>
    <div className="row text-center">
      <h4><Link to="">{"Go home"}</Link></h4>
    </div>
  </div>
);

NotFound.propTypes = {
  location: React.PropTypes.object.isRequired
};

export default rootComponent(NotFound);
export const NotFoundNoRoot = NotFound;
