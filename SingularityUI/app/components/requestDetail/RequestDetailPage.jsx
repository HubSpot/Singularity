import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

const RequestDetailPage = ({requestAPI}) => {
  let headerData = <h1>Singularity</h1>;
  const deployUser = Utils.maybe(requestAPI.data, [
    'user',
    'id'
  ]);
  if (deployUser) {
    headerData = <h1>{deployUser}</h1>;
  }

  return (
    <header>
      {headerData}
    </header>
  );
};

RequestDetailPage.propTypes = {
  userAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => {
  return {
    request: state.api.request[ownProps.requestId]
  };
};

export default connect(
  mapStateToProps
)(RequestDetailPage);
