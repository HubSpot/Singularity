from setuptools import setup, find_packages

requirements = [
  'argparse==1.2.2',
  'ConfigParser==3.5.0b2',
  'grequests==0.2.0',
  'requests==2.5.0',
  'termcolor==1.1.0'
]

setup(
    name='singularity-logfetch',
    version='0.13.0',
    description='Singularity log fetching and searching',
    author="HubSpot",
    author_email='singularity-users@googlegroups.com',
    url='https://github.com/HubSpot/Singularity',
    packages=find_packages(),
    include_package_data=True,
    install_requires=requirements,
    zip_safe=False,
    entry_points={
        'console_scripts':[
            'logfetch=logfetch.entrypoint:fetch',
            'logtail=logfetch.entrypoint:tail',
            'logcat=logfetch.entrypoint:cat'
        ],
    }
)
