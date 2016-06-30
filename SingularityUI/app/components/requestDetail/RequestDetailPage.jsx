import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import RequestHeader from './RequestHeader';

const RequestDetailPage = ({requestParent}) => {
  // if deleted handle differently here
  return (
    <div>
      <RequestHeader requestParent={requestParent} />
    </div>
  );
};

RequestDetailPage.propTypes = {
  requestParent: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => {
  return {
    requestParent: state.api.request[ownProps.requestId].data
  };
};

export default connect(
  mapStateToProps
)(RequestDetailPage);
