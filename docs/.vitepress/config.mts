import { defineConfig } from 'vitepress'

export default defineConfig({
    title: "Plots",
    description: "Advanced Plot System for Hytale",
    base: '/plots/', // Adjust based on your GitHub repo name
    themeConfig: {
        logo: '/logo.png',
        nav: [
            { text: 'Home', link: '/' },
            { text: 'Guide', link: '/guide/intro/getting-started' },
        ],
        sidebar: [
            { text: 'README', link: '/guide/intro/readme' },
            {
                text: 'Introduction',
                items: [
                    { text: 'What is Plots?', link: '/guide/intro/what-is-plots' },
                    { text: 'Getting Started', link: '/guide/intro/getting-started' },
                ]
            },
            {
                text: 'Features',
                items: [
                    { text: 'World Generation', link: '/guide/features/world-gen' },
                    { text: 'Plot Protection', link: '/guide/features/protection' },
                    { text: 'Plot Flags', link: '/guide/features/flags' },
                ]
            },
            {
                text: 'Configuration',
                items: [
                    { text: 'General Settings', link: '/guide/setup/config' },
                    { text: 'Translations', link: '/guide/setup/translations' },
                    { text: 'Prefabs', link: '/guide/setup/prefabs-customization' },
                ]
            },
            {
                text: 'Developer API',
                items: [
                    { text: 'Overview', link: '/guide/api/api' },
                    { text: 'Events', link: '/guide/api/api-events' },
                    { text: 'Usage & Dependency', link: '/guide/api/api-usage' },
                ]
            },
            {
                text: 'Integrations',
                items: [
                    { text: 'Hylograms', link: '/guide/integrations/hylograms' },
                ]
            },
            {
                text: 'Reference',
                items: [
                    { text: 'Commands', link: '/guide/reference/commands' },
                    { text: 'Permissions', link: '/guide/reference/permissions' },
                ]
            }
        ],
        socialLinks: [
            { icon: 'github', link: 'https://github.com/overworldlabs/plots' }
        ],
        lastUpdated: {
            text: 'Last Updated',
            formatOptions: {
                dateStyle: 'short',
                timeStyle: 'short'
            }
        },
        editLink: {
            pattern: 'https://github.com/overworldlabs/plots/edit/main/docs/:path',
            text: 'Edit this page on GitHub'
        },
        footer: {
            message: 'Released under the MIT License.',
            copyright: 'Copyright © 2026-present Overworld Labs'
        },
        search: {
            provider: 'local'
        }
    }
})
