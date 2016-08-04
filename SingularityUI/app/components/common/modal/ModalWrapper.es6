import React from 'react';

export const getClickComponent = (component, doFirst) => (
  React.Children.map(component.props.children, child => (
    React.cloneElement(child, {
      onClick: () => {
        if (doFirst) {
          doFirst().then(() => component.refs.modal.getWrappedInstance().show());
        } else {
          component.refs.modal.getWrappedInstance().show();
        }
      }
    })
  ))
);
