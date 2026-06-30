// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer').themes.github;
const darkCodeTheme = require('prism-react-renderer').themes.dracula;

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Quarkus Auth Library',
  tagline: 'Learn how Quarkus Auth Library works',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'http://localhost',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'quarkus-auth', // Usually your GitHub org/user name.
  projectName: 'quarkus-auth-doc', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
     
      navbar: {
        title: 'Quarkus Auth Library',
        logo: {
          alt: 'Quarkus Auth',
          src: 'img/logo.svg',
          srcDark: 'img/logodark.svg',
        },
        items: [
        {
  type: 'doc',
  docId: 'introduction',
  position: 'left',
  label: 'Documentation',
}
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Explore the Documentation',
                to: '/docs/introduction',
              },
            ],
          },
          {
            title: 'Repos',
            items: [
              {
                label: 'Quarkus Auth Library',
                to: 'https://github.com/ARGOeu/quarkus-auth',
              }            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} fc4e-cat`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),

    themes: [
      [
        "@easyops-cn/docusaurus-search-local",
        {
          hashed: true,
          language: ["en", "zh"],
          highlightSearchTermsOnTargetPage: true,
          explicitSearchResultPath: true,
          indexBlog: false,
        },
      ],
    ],
};

module.exports = config;
