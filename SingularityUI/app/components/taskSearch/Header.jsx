import React, { PropTypes } from 'react';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

const Header = (props) => {
  let maybeLink;

  if (props.global) {
    maybeLink = (
      <a className="btn btn-danger" href={`${config.appRoot}/request/${props.requestId}`} alt={`Return to Request ${props.requestId}`}>
        <Glyphicon iconClass="arrow-left" />
        {' Back to '}{props.requestId}
      </a>
    );
  }
  return (
    <div>
      {maybeLink}
      <h1>
        {props.global ? 'Global' : ''} Historical Tasks
      </h1>
    </div>
  );
};

Header.propTypes = {
  global: PropTypes.bool,
  requestId: PropTypes.string
};

export default Header;
