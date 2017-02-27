const gulp        = require('gulp')
const rollup      = require('rollup-stream')
const sourcemaps  = require('gulp-sourcemaps')
const typescript  = require('rollup-plugin-typescript')
const source      = require('vinyl-source-stream')
const buffer      = require('vinyl-buffer')
const concat      = require('gulp-concat')
const compressJS  = require('gulp-uglify')
const sass        = require('gulp-sass')
const compressCSS = require('gulp-clean-css')
const del         = require('del')

// Compile Typescript files to ES6 and combine modules with Rollup.
gulp.task('compile:js', () => {
  return rollup({
      entry: './src/typescript/app.ts',
      moduleName: 'frontend',
      format: 'iife',
      sourceMap: true,
      plugins: [
        typescript()
      ],
      external: [
        'codemirror',
        'codemirror-no-newlines',
        'localforage'
      ],
      globals: {
        codemirror: 'CodeMirror',
        localforage: 'localforage'
      }
    })
    .pipe(source('app.js'))
    .pipe(buffer())
    .pipe(sourcemaps.init({loadMaps: true}))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest('./dist'))
})

// Concatenate JS libraries into a single file.
gulp.task('bundle:js', () => {
  // Paths to libraries used by the frontend. Each file in this list is
  // concatenated into ./dist/libs.js and will be concatenated in this order:
  const libs = [
    './node_modules/codemirror/lib/codemirror.js',
    './node_modules/codemirror/addon/display/placeholder.js',
    './node_modules/codemirror-no-newlines/no-newlines.js',
    './node_modules/localforage/dist/localforage.nopromises.min.js'
  ]

  return gulp.src(libs)
    .pipe(concat('libs.js'))
    .pipe(gulp.dest('./dist'))
})

// Uglify app logic and concatenated libraries.
gulp.task('compress:js', ['compile:js', 'bundle:js'], () => {
  return gulp.src('./dist/*.js')
    .pipe(compressJS())
    .pipe(gulp.dest('./dist'))
})

// Compile SCSS files to CSS.
gulp.task('compile:css', () => {
  return gulp.src('src/sass/*.scss')
    .pipe(sass().on('error', sass.logError))
    .pipe(gulp.dest('./dist'))
})

// Concatenate CSS stylesheets used by libraries.
gulp.task('bundle:css', () => {
  // Paths to stylesheets used by the frontend. Each file in this list is
  // concatenated into ./dist/libs.css and will be concatenated in this order:
  const libs = [
    './node_modules/codemirror/lib/codemirror.css'
  ]

  return gulp.src(libs)
    .pipe(concat('libs.css'))
    .pipe(gulp.dest('./dist'))
})

// Compress app & library stylesheets.
gulp.task('compress:css', ['compile:css', 'bundle:css'], () => {
  return gulp.src('./dist/*.css')
    .pipe(compressCSS())
    .pipe(gulp.dest('./dist'))
})

gulp.task('build:all', ['compress:css', 'compress:js'])

gulp.task('watch:js', () => {
  gulp.watch('src/typescript/*.ts', ['compile:js'])
})

gulp.task('watch:css', () => {
  gulp.watch('src/sass/*.scss', ['compile:css'])
})

// Delete everything inside the ./dist directory including the directory itself.
gulp.task('clean', () => {
  return del([ './dist' ])
})
