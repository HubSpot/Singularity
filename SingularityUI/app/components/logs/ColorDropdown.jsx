import React, { PropTypes } from 'react';
import classNames from 'classnames';

import { connect } from 'react-redux';
import { selectLogColor } from '../../actions/log';

function ColorDropdown(props) {
  function renderColorChoices() {
    const { activeColor } = props;

    return props.colors.map((color, index) => {
      const colorClass = color.toLowerCase().replace(' ', '-');
      const className = classNames({ active: activeColor === colorClass });
      return React.createElement('li', { 'key': index, className }, React.createElement('a', { ['onClick']: () => props.selectLogColor(colorClass) }, color));
    });
  }

  return (
    <div className="btn-group" title="Select Color Scheme">
      <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-adjust" /> <span className="caret" />
      </button>
      <ul className="dropdown-menu dropdown-menu-right">
        {renderColorChoices()}
      </ul>
    </div>
  );
}

ColorDropdown.propTypes = {
  colors: PropTypes.arrayOf(PropTypes.string).isRequired,
  selectLogColor: PropTypes.func.isRequired,
  activeColor: PropTypes.string.isRequired
};

const mapStateToProps = state => ({
  colors: state.colors,
  activeColor: state.activeColor
});

const mapDispatchToProps = { selectLogColor };

export default connect(mapStateToProps, mapDispatchToProps)(ColorDropdown);
