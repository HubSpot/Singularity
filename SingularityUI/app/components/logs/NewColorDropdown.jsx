import React, { PropTypes } from 'react';
import classNames from 'classnames';

const COLORS = ['Default', 'Light', 'Dark'];

const NewColorDropdown = ({activeColor, onSetColor}) => {
  const renderColorItem = (name, key) => (<li key={key} className={classNames({ active: activeColor === name.toLowerCase() })}>
    <a onClick={() => onSetColor(name.toLowerCase())}>{name}</a>
  </li>);

  return (<div className="btn-group" title="Select Color Scheme">
    <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-adjust" /> <span className="caret" />
      </button>
      <ul className="dropdown-menu dropdown-menu-right">
        {COLORS.map(renderColorItem)}
      </ul>
  </div>);
}

NewColorDropdown.propTypes = {
  onSetColor: PropTypes.func.isRequired,
  activeColor: PropTypes.string.isRequired
};

export default NewColorDropdown;