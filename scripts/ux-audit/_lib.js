// Shared helpers. Each scenario file inlines what it needs, because
// browser_run_code_unsafe runs one self-contained function per file.
// Edit BASE / PASSWORD here, then copy into scenarios if they change.
module.exports = {
  BASE: "http://localhost:3100",
  PASSWORD: "Demo@12345",
  ACCOUNTS: {
    user: "demo.user1@petrescue.demo",
    owner: "demo.owner1@petrescue.demo",
    admin: "demo.admin@petrescue.demo",
  },
};
