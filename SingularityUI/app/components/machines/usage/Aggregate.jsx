import React, { PropTypes } from 'react';
import classNames from 'classnames';
import CircularProgressbar from 'react-circular-progressbar';
import {Link} from 'react-router';

const Aggregate = ({width, vcenter, graph, className, value, label, link}) => {
  const valueComponent = (
      graph ?
        <CircularProgressbar percentage={value} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} /> :
        <div className={classNames('value', {[className]: className})}>
          {value}
        </div>
  );
  const labelComponent = (
    <div className={classNames('label', {[className]: className})}>
      {label}
    </div>
  );

  return (
    <div className={classNames(
      'aggregate',
      `col-md-${width}`,
      {vcenter},
      {graph}
    )}>
      {link ?
        <Link to={link}>
          {valueComponent}
          {labelComponent}
        </Link> :
        <div>
          {valueComponent}
          {labelComponent}
        </div>
      }
    </div>
  );
};

Aggregate.propTypes = {
  width: PropTypes.number.isRequired,
  vcenter: PropTypes.bool,
  graph: PropTypes.bool,
  className: PropTypes.string,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  label: PropTypes.string.isRequired,
  link: PropTypes.string
};

Aggregate.defaultProps = {
  vcenter: false,
  graph: false
};

export default Aggregate;
