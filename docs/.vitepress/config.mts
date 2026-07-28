import { defineConfig } from 'vitepress'

// Plots documentation — plain VitePress. The docs always describe the current release; a
// hand-maintained Changelog page (guide/changelog.md) records what changed per version.
export default defineConfig({
    title: 'Plots',
    description: 'Grid-based plot management for Hytale — claim, protect, merge, and run whole plot worlds.',
    base: '/plots/', // GitHub Pages: https://stoshelabs.github.io/plots/
    lang: 'en-US',
    cleanUrls: true,
    lastUpdated: true,
    head: [
        ['link', { rel: 'icon', href: '/plots/icon.png' }],
        ['meta', { name: 'theme-color', content: '#8b2bff' }],
        ['meta', { property: 'og:title', content: 'Plots — Grid Plot Management for Hytale' }],
        ['meta', { property: 'og:description', content: 'Grid-based plot management for Hytale: claim, auto-claim, trust, flags, merge/unmerge, multi-world, BuilderTools-safe protection, economy and a public API.' }],
        ['meta', { property: 'og:image', content: '/plots/logo.png' }],
    ],
    themeConfig: {
        logo: '/icon.png',
        nav: [
            { text: 'Home', link: '/' },
            { text: 'Guide', link: '/guide/intro/what-is-plots' },
            { text: 'Commands', link: '/guide/reference/commands' },
            { text: 'Changelog', link: '/guide/changelog' },
        ],
        sidebar: [
            {
                text: 'Introduction',
                items: [
                    { text: 'What is Plots?', link: '/guide/intro/what-is-plots' },
                    { text: 'Getting Started', link: '/guide/intro/getting-started' },
                ],
            },
            {
                text: 'Features',
                items: [
                    { text: 'World Generation', link: '/guide/features/world-gen' },
                    { text: 'Multi-World', link: '/guide/features/multiworld' },
                    { text: 'Plot Protection', link: '/guide/features/protection' },
                    { text: 'Plot Flags', link: '/guide/features/flags' },
                ],
            },
            {
                text: 'Configuration',
                items: [
                    { text: 'Config Reference', link: '/guide/setup/config' },
                    { text: 'Translations', link: '/guide/setup/translations' },
                    { text: 'Prefabs', link: '/guide/setup/prefabs-customization' },
                ],
            },
            {
                text: 'Integrations',
                items: [
                    { text: 'TaleGuard', link: '/guide/integrations/taleguard' },
                    { text: 'Hylograms', link: '/guide/integrations/hylograms' },
                    { text: 'PlaceholderAPI', link: '/guide/integrations/placeholders' },
                ],
            },
            {
                text: 'Developer API',
                items: [
                    { text: 'Overview', link: '/guide/api/api' },
                    { text: 'Events', link: '/guide/api/api-events' },
                    { text: 'Usage & Dependency', link: '/guide/api/api-usage' },
                ],
            },
            {
                text: 'Reference',
                items: [
                    { text: 'Commands', link: '/guide/reference/commands' },
                    { text: 'Permissions', link: '/guide/reference/permissions' },
                ],
            },
            {
                text: 'Releases',
                items: [
                    { text: 'Changelog', link: '/guide/changelog' },
                ],
            },
        ],
        socialLinks: [
            { icon: 'github', link: 'https://github.com/stoshelabs/plots' },
            { icon: 'discord', link: 'https://discord.gg/rC9eSzH3tf' },
        ],
        search: {
            provider: 'local',
        },
        editLink: {
            pattern: 'https://github.com/stoshelabs/plots/edit/main/docs/:path',
            text: 'Edit this page on GitHub',
        },
        lastUpdated: {
            text: 'Last updated',
            formatOptions: { dateStyle: 'short', timeStyle: 'short' },
        },
        footer: {
            message: 'Released under the MIT License.',
            copyright: 'Copyright © 2026-present Stoshe Labs',
        },
    },
})
