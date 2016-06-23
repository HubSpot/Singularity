import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

const Header = ({userAPI}) => {
  let headerData = <h1>Singularity</h1>;
  const deployUser = Utils.maybe(userAPI.data, [
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

Header.propTypes = {
  userAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state) => {
  return {
    userAPI: state.api.user
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
  };
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Header);
